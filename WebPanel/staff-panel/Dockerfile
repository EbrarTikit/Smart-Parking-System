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