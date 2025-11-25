# 构建阶段
# 代理配置（可选）：如果需要代理，通过 --build-arg 传递
# 如果不需要代理，ARG 默认为空，不会影响构建
ARG HTTP_PROXY
ARG HTTPS_PROXY
FROM node:20-alpine AS build
# 设置代理环境变量（如果未传递 ARG，值为空，不影响构建）
ENV HTTP_PROXY=${HTTP_PROXY}
ENV HTTPS_PROXY=${HTTPS_PROXY}

WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
