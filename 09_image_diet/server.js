const express = require('express');
const app = express();

app.get('/', (req, res) => {
    res.json({ status: "ok", app: "diet-test", message: "Hello from minimal Docker image!" });
});

app.listen(8080, () => console.log("Diet Server running on port 8080"));
