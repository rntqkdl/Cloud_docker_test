const express = require('express');
const mysql   = require('mysql2');
const os      = require('os');
const app     = express();

const db = mysql.createConnection({
    host:     'db',
    user:     'user',
    password: 'password123',
    database: 'skala'
});

app.get('/users', (req, res) => {
    db.query('SELECT id, username, email FROM users', (err, rows) => {
        if (err) return res.status(500).json({ error: err.message });
        res.json({
            container: os.hostname(),
            message: "Docker Compose 3-Tier 연동 성공!",
            users: rows
        });
    });
});

app.get('/whoami', (req, res) => {
    res.json({ container: os.hostname() });
});

app.listen(8080, () => console.log('WAS started on ' + os.hostname()));
