# Backend — Lista de la compra familiar

Backend en Spring Boot + MongoDB, alojado en tu propio servidor Ubuntu
(sin interfaz gráfica) y expuesto a internet con ngrok. Implementa el mismo
contrato de API que ya usa la app Android (REST + streaming SSE en tiempo real).

## Resumen de la arquitectura

```
[App Android] --https--> [ngrok, URL pública fija] --túnel--> [tu servidor Ubuntu]
                                                                      |
                                                          Backend Spring Boot :8080
                                                                      |
                                                          MongoDB (Docker) :27017
```

Tanto el backend como MongoDB corren en tu servidor. ngrok solo abre una
puerta pública hacia el puerto 8080, sin que tengas que tocar el router
ni abrir puertos manualmente (nada de "port forwarding").

---

## 1. Requisitos previos en el servidor

Conéctate por SSH a tu servidor Ubuntu y comprueba/instala lo necesario:

```bash
# Java 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# Maven (para compilar el proyecto)
sudo apt install -y maven

# Docker (para MongoDB)
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker

# Permite ejecutar docker sin sudo (opcional pero cómodo)
sudo usermod -aG docker $USER
# cierra sesión y vuelve a entrar por SSH para que el cambio de grupo aplique
```

Verifica versiones:
```bash
java -version      # debe ser 17 o superior
mvn -version
docker --version
```

## 2. Subir el proyecto al servidor

Desde tu ordenador, copia la carpeta `backend` al servidor (ajusta usuario e IP):

```bash
scp -r backend usuario@IP_DEL_SERVIDOR:/home/usuario/
```

O si prefieres usar git, sube el proyecto a GitHub desde tu PC y clónalo en
el servidor con `git clone`.

## 3. Levantar MongoDB con Docker

Desde la carpeta `backend` en el servidor:

```bash
cd backend
docker compose up -d
```

Comprueba que está corriendo:
```bash
docker ps
```
Deberías ver un contenedor llamado `lista-compra-mongo`.

## 4. Compilar el backend

```bash
mvn clean package -DskipTests
```

Esto genera `target/backend-1.0.0.jar`. Pruébalo manualmente antes de
convertirlo en servicio:

```bash
java -jar target/backend-1.0.0.jar
```

Deberías ver logs de Spring Boot arrancando y, al final, algo como
`Started BackendApplication`. Prueba desde otra terminal (o desde tu
ordenador si el puerto es accesible en tu red):

```bash
curl http://localhost:8080/api/lists
```
Debe devolver `[]`. Si funciona, para el proceso con `Ctrl+C` y sigue al
siguiente paso para convertirlo en un servicio permanente.

## 5. Configurar el backend como servicio systemd

Edita `systemd/lista-compra-backend.service` (dentro del proyecto) y
sustituye `TU_USUARIO` por tu usuario real de Ubuntu y ajusta la ruta si
subiste el proyecto a otro sitio distinto de `/home/TU_USUARIO/backend`.

Cópialo al sitio donde systemd busca los servicios:

```bash
sudo cp systemd/lista-compra-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable lista-compra-backend
sudo systemctl start lista-compra-backend
```

Comprueba que está corriendo:
```bash
sudo systemctl status lista-compra-backend
```

Ver logs en cualquier momento:
```bash
sudo journalctl -u lista-compra-backend -f
```

A partir de ahora, el backend arrancará solo si reinicias el servidor, y
si el proceso muere por cualquier motivo, systemd lo reinicia automáticamente
a los 5 segundos (`Restart=on-failure`).

## 6. Instalar y configurar ngrok

**Crea una cuenta gratuita** en https://dashboard.ngrok.com/signup

**Instala ngrok en el servidor:**
```bash
curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
  | sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null
echo "deb https://ngrok-agent.s3.amazonaws.com buster main" \
  | sudo tee /etc/apt/sources.list.d/ngrok.list
sudo apt update && sudo apt install ngrok
```

**Conecta tu cuenta** (el token lo encuentras en
https://dashboard.ngrok.com/get-started/your-authtoken):
```bash
ngrok config add-authtoken TU_TOKEN_AQUI
```

**Reserva un dominio estático gratuito** (para que la URL nunca cambie):
1. Ve a https://dashboard.ngrok.com/domains
2. Clic en **New Domain** — te asigna algo como `algo-random.ngrok-free.app`
   (en el plan gratuito no puedes elegir el nombre, pero es fijo una vez asignado)
3. Copia ese dominio

**Prueba el túnel manualmente:**
```bash
ngrok http 8080 --domain=TU_DOMINIO_ASIGNADO.ngrok-free.app
```
Deja esto corriendo y, desde tu móvil (con datos, no WiFi, para probar que
es de verdad público), abre en el navegador:
```
https://TU_DOMINIO_ASIGNADO.ngrok-free.app/api/lists
```
Debe devolver `[]`. Si funciona, para el proceso con `Ctrl+C` y convierte
el túnel en un servicio permanente también.

## 7. Configurar ngrok como servicio systemd

Edita `systemd/ngrok-tunnel.service` y sustituye `TU_DOMINIO` y `TU_USUARIO`
por los tuyos reales.

```bash
sudo cp systemd/ngrok-tunnel.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable ngrok-tunnel
sudo systemctl start ngrok-tunnel
```

Comprueba:
```bash
sudo systemctl status ngrok-tunnel
sudo journalctl -u ngrok-tunnel -f
```

## 8. Configurar la app Android

En la pantalla de configuración de la app, introduce:
```
https://TU_DOMINIO_ASIGNADO.ngrok-free.app/
```

Como es HTTPS de verdad (ngrok se encarga del certificado), no necesitas
tocar nada de `cleartextTraffic` — eso solo era necesario para el backend
fake local en HTTP.

---

## Comandos útiles del día a día

```bash
# Ver estado de todo
sudo systemctl status lista-compra-backend
sudo systemctl status ngrok-tunnel
docker ps

# Reiniciar el backend tras un cambio de código
mvn clean package -DskipTests
sudo systemctl restart lista-compra-backend

# Ver logs en vivo
sudo journalctl -u lista-compra-backend -f
sudo journalctl -u ngrok-tunnel -f

# Parar todo
sudo systemctl stop lista-compra-backend
sudo systemctl stop ngrok-tunnel
docker compose down
```

## Endpoints disponibles

```
GET    /api/lists
POST   /api/lists                     { name }
DELETE /api/lists/{listId}

GET    /api/lists/{listId}/products
POST   /api/lists/{listId}/products   { name, quantity }
PUT    /api/products/{productId}      { name?, quantity?, checked? }
DELETE /api/products/{productId}

GET    /api/events                    (SSE stream)
```
