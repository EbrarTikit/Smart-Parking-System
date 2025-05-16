# Çok aşamalı yapı - İlk aşama build
FROM node:18-alpine as build

# Çalışma dizini
WORKDIR /app

# Bağımlılıkları kopyala ve yükle
COPY package.json package-lock.json ./
RUN npm ci

# Kaynak kodları kopyala
COPY . .

# Uygulamayı derle
RUN npm run build

# İkinci aşama - Nginx ile servis etme (ÜRETİM)
FROM nginx:stable-alpine as production

# Nginx yapılandırmasını kopyala
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Derlenmiş uygulamayı Nginx dizinine kopyala
COPY --from=build /app/build /usr/share/nginx/html

# Port 80 üzerinden servis et
EXPOSE 80

# Nginx'i başlat
CMD ["nginx", "-g", "daemon off;"]

# Geliştirme aşaması - Hot reloading destekli (GELİŞTİRME)
FROM node:18-alpine as development

WORKDIR /app

# Bağımlılıkları kopyala ve yükle
COPY package.json package-lock.json ./
RUN npm install

# Kaynak kodları kopyala
COPY . .

# Geliştirme portu
EXPOSE 3000

# Geliştirme sunucusunu başlat
CMD ["npm", "start"] 