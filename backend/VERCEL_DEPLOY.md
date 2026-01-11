# 🚀 Deploy Backend su Vercel - Guida Completa

## 📋 Prerequisiti

1. **Account Vercel**: Crea un account gratuito su [vercel.com](https://vercel.com)
2. **Vercel CLI** (opzionale ma consigliato):
   ```bash
   npm install -g vercel
   ```

---

## 🌐 Metodo 1: Deploy via Web (Più Semplice)

### Passo 1: Prepara il Repository
Il backend è già pronto! I file necessari sono stati creati:
- ✅ `vercel.json` - Configurazione Vercel
- ✅ `.vercelignore` - File da escludere
- ✅ `server.js` - Modificato per serverless

### Passo 2: Carica su GitHub (se non l'hai già fatto)
```bash
cd backend
git init
git add .
git commit -m "Prepare backend for Vercel"
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```

### Passo 3: Importa su Vercel
1. Vai su [vercel.com/new](https://vercel.com/new)
2. Clicca **"Import Git Repository"**
3. Seleziona il tuo repository GitHub
4. **IMPORTANTE**: Cambia **Root Directory** a `backend`
5. Clicca **"Deploy"**

### Passo 4: Configura Variabili d'Ambiente
Dopo il primo deploy (che probabilmente fallirà), vai in:
1. **Dashboard Vercel** → **Settings** → **Environment Variables**
2. Aggiungi le seguenti variabili:

```
DB_HOST=57.129.5.234
DB_PORT=5432
DB_NAME=rfid_db
DB_USER=rfidmanager
DB_PASSWORD=iniAD16Z77oS
CORS_ORIGIN=*
READER_ID=RFD8500-DEFAULT
```

3. Clicca **"Redeploy"** per applicare le variabili

---

## 💻 Metodo 2: Deploy via CLI (Più Veloce)

### Passo 1: Login a Vercel
```bash
vercel login
```

### Passo 2: Deploy da Terminale
```bash
cd backend
vercel
```

Rispondi alle domande:
- **Set up and deploy?** → Yes
- **Which scope?** → Seleziona il tuo account
- **Link to existing project?** → No
- **Project name?** → `rfid-backend` (o altro nome)
- **In which directory?** → `./` (è già nella cartella backend)
- **Override settings?** → No

### Passo 3: Aggiungi Variabili d'Ambiente
```bash
vercel env add DB_HOST
# Inserisci: 57.129.5.234

vercel env add DB_PORT
# Inserisci: 5432

vercel env add DB_NAME
# Inserisci: rfid_db

vercel env add DB_USER
# Inserisci: rfidmanager

vercel env add DB_PASSWORD
# Inserisci: iniAD16Z77oS

vercel env add CORS_ORIGIN
# Inserisci: *

vercel env add READER_ID
# Inserisci: RFD8500-DEFAULT
```

### Passo 4: Redeploy con Variabili
```bash
vercel --prod
```

---

## 🔗 Ottenere l'URL del Backend

Dopo il deploy, Vercel ti fornirà un URL tipo:
```
https://rfid-backend-xxx.vercel.app
```

**Questo è il tuo BASE_URL per l'app Android!**

---

## 📱 Aggiornare App Android con Nuovo URL

### File da Modificare: `RetrofitClient.kt`

```kotlin
// android-app/app/src/main/java/com/rfid/reader/network/RetrofitClient.kt

object RetrofitClient {
    // ✅ SOSTITUISCI con il tuo URL Vercel
    private const val BASE_URL = "https://rfid-backend-xxx.vercel.app/"

    // ... resto del codice
}
```

**Rebuild l'APK Android** dopo la modifica.

---

## ✅ Testare il Backend su Vercel

### 1. Test Health Check
```bash
curl https://rfid-backend-xxx.vercel.app/health
```

Dovresti vedere:
```json
{
  "status": "OK",
  "timestamp": "2024-01-11T10:00:00.000Z",
  "database": {
    "host": "57.129.5.234",
    "database": "rfid_db"
  }
}
```

### 2. Test API
```bash
curl https://rfid-backend-xxx.vercel.app/api/places
```

Dovresti ricevere la lista dei places dal database.

---

## 🔄 Deploy Successivi

### Metodo Web (GitHub):
- Fai commit e push su GitHub
- Vercel redeploya automaticamente

### Metodo CLI:
```bash
cd backend
vercel --prod
```

---

## ⚠️ Possibili Problemi e Soluzioni

### Errore: "Cannot connect to database"
✅ **Soluzione**: Verifica le variabili d'ambiente su Vercel Dashboard

### Errore: "Function timeout"
✅ **Soluzione**: Vercel free tier ha timeout di 10 secondi. Ottimizza le query se necessario.

### Errore: "CORS policy"
✅ **Soluzione**: Assicurati che `CORS_ORIGIN=*` sia configurato nelle variabili d'ambiente

### Errore: "Module not found"
✅ **Soluzione**: Assicurati che tutte le dependencies siano in `package.json` (non in devDependencies)

---

## 📊 Monitoraggio

**Dashboard Vercel** ti fornisce:
- Logs in tempo reale
- Metriche di traffico
- Error tracking
- Analytics

Accedi a: `https://vercel.com/dashboard`

---

## 💰 Limiti Vercel Free Tier

- ✅ 100 GB bandwidth/mese
- ✅ 100,000 invocazioni serverless/giorno
- ✅ 10 secondi timeout funzioni
- ✅ Deployment illimitati

**Per l'uso interno dell'app RFID è più che sufficiente!**

---

## 🎉 Fatto!

Il tuo backend è ora su Vercel con:
- ✅ URL pubblico HTTPS
- ✅ Auto-scaling
- ✅ Deploy automatici da Git
- ✅ Database PostgreSQL remoto funzionante
- ✅ Zero configurazione server

**Prossimo step**: Aggiorna `BASE_URL` nell'app Android e rebuilda l'APK!
