#!/bin/sh

# Tüm environment değişkenlerini nginx yapılandırmasına ekle
envsubst '${REACT_APP_AUTH_SERVICE_URL} ${REACT_APP_PARKING_SERVICE_URL} ${REACT_APP_NAME} ${REACT_APP_VERSION} ${REACT_APP_ENABLE_ANALYTICS} ${REACT_APP_ENABLE_NOTIFICATIONS}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf

# Nginx'i başlat
exec nginx -g 'daemon off;'
