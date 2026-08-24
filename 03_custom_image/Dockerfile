# 1. 바탕이 되는 초경량 Node.js 환경
FROM node:18-alpine

# 2. 내 컴퓨터의 app.js 파일을 이미지 안으로 복사
COPY app.js .

# 3. 컨테이너가 켜질 때 실행할 명령어
CMD ["node", "app.js"]
