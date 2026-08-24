const express = require('express');
const mysql   = require('mysql2/promise');
const os      = require('os');
const cors    = require('cors');

const app = express();
app.use(cors());
app.use(express.json());

const DB_CONFIG = {
    host:     process.env.DB_HOST || 'db',
    port:     parseInt(process.env.DB_PORT || '3306'),
    user:     process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || 'password123',
    database: process.env.DB_NAME || 'dashboard_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

let pool;
async function initDbPool() {
    try {
        pool = mysql.createPool(DB_CONFIG);
        const [rows] = await pool.query('SELECT 1 + 1 AS test');
        console.log(`[WAS] Connected to MariaDB successfully at ${DB_CONFIG.host}:${DB_CONFIG.port}`);
    } catch (err) {
        console.error('[WAS] Initial DB connection failed, retrying on demand:', err.message);
    }
}
initDbPool();

// 1. 실시간 헬스체크 & 지연시간 진단 API
app.get('/api/health', async (req, res) => {
    const startTime = Date.now();
    try {
        if (!pool) pool = mysql.createPool(DB_CONFIG);
        const [rows] = await pool.query('SELECT NOW() AS server_time, VERSION() AS db_version');
        const latencyMs = Date.now() - startTime;
        res.json({
            status: 'healthy',
            container: os.hostname(),
            node_version: process.version,
            database: {
                connected: true,
                latency_ms: latencyMs,
                server_time: rows[0].server_time,
                version: rows[0].db_version
            }
        });
    } catch (err) {
        res.status(500).json({
            status: 'degraded',
            container: os.hostname(),
            error: err.message
        });
    }
});

// 2. 퀘스트 진척도 조회 API
app.get('/api/progress/:userId', async (req, res) => {
    try {
        const userId = req.params.userId || 'skala-g124';
        const [rows] = await pool.query('SELECT * FROM quest_progress WHERE user_id = ?', [userId]);
        if (rows.length === 0) {
            return res.json({
                user_id: userId,
                user_name: '4반 G124 안성민',
                current_level: 1,
                completed_labs: ['lab1']
            });
        }
        res.json({
            ...rows[0],
            completed_labs: typeof rows[0].completed_labs === 'string' ? JSON.parse(rows[0].completed_labs) : rows[0].completed_labs
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 3. 퀘스트 진척도 저장/업데이트 API
app.post('/api/progress', async (req, res) => {
    try {
        const { userId, userName, level, completedLabs } = req.body;
        const labsJson = JSON.stringify(completedLabs || []);
        
        await pool.query(
            `INSERT INTO quest_progress (user_id, user_name, current_level, completed_labs)
             VALUES (?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE user_name = VALUES(user_name), current_level = VALUES(current_level), completed_labs = VALUES(completed_labs)`,
            [userId || 'skala-g124', userName || '4반 G124 안성민', level || 1, labsJson]
        );

        res.json({
            success: true,
            message: '퀘스트 진척도가 MariaDB에 영구 저장되었습니다!',
            container: os.hostname()
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 4. 실습 Q&A / 공부 메모 목록 조회 API
app.get('/api/notes', async (req, res) => {
    try {
        const [rows] = await pool.query('SELECT * FROM study_notes ORDER BY id DESC LIMIT 20');
        res.json({
            container: os.hostname(),
            count: rows.length,
            notes: rows
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// 5. 실습 Q&A / 공부 메모 작성 API
app.post('/api/notes', async (req, res) => {
    try {
        const { author, category, content } = req.body;
        if (!author || !content) {
            return res.status(400).json({ error: '작성자와 내용을 입력해주세요.' });
        }
        await pool.query(
            'INSERT INTO study_notes (author, category, content) VALUES (?, ?, ?)',
            [author, category || '실습 팁', content]
        );
        res.json({
            success: true,
            message: '새로운 공부 메모가 MariaDB에 기록되었습니다!',
            container: os.hostname()
        });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

const PORT = 8080;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`[WAS] Server running on http://0.0.0.0:${PORT} (Container: ${os.hostname()})`);
});
