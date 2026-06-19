# WebChat Client

A React-based web application with JWT authentication, designed to work with the WebChat backend.

## 📋 Features

- **User Authentication**
  - Sign up with phone number, password, and display name
  - Login with phone number and password
  - JWT token-based authentication
  - Persistent authentication state
  - Logout functionality

- **User Dashboard**
  - Welcome message with user information
  - User profile display
  - Responsive design
  - Clean and modern UI

- **Security**
  - JWT tokens stored in localStorage
  - Protected routes
  - Automatic redirects for unauthorized access
  - CORS proxy configuration

## 🛠 Tech Stack

- **Frontend Framework**: React 18.3
- **Routing**: React Router v6
- **HTTP Client**: Axios
- **Build Tool**: Vite
- **Styling**: CSS3 with modern design patterns

## 📦 Installation

1. Navigate to the client directory:
```bash
cd webchat-client
```

2. Install dependencies:
```bash
npm install
```

## 🚀 Development

Start the development server:
```bash
npm run dev
```

The application will start at `http://localhost:3000`

The development server includes a proxy that redirects API calls from `/api` to `http://localhost:8080`.

## 🏗 Build

Create a production build:
```bash
npm run build
```

Preview the production build:
```bash
npm run preview
```

## 📁 Project Structure

```
webchat-client/
├── public/                    # Static assets
├── src/
│   ├── components/            # Reusable React components
│   ├── pages/
│   │   ├── Login.jsx         # Login page
│   │   ├── Signup.jsx        # Signup/registration page
│   │   └── Dashboard.jsx     # Main dashboard (protected)
│   ├── services/
│   │   └── authService.js    # API service for authentication
│   ├── styles/
│   │   ├── Auth.css          # Authentication pages styling
│   │   └── Dashboard.css     # Dashboard styling
│   ├── App.jsx               # Main app component with routing
│   ├── App.css               # App styling
│   ├── index.css             # Global styles
│   └── main.jsx              # React entry point
├── index.html                # HTML template
├── vite.config.js           # Vite configuration
├── package.json             # Dependencies and scripts
└── .gitignore              # Git ignore rules
```

## 🔐 Authentication Flow

1. **Sign Up**: User registers with phone number, password, and display name
   - `POST /api/auth/signup`
   - Returns JWT token, phone number, and display name
   - Token stored in localStorage

2. **Login**: User logs in with phone number and password
   - `POST /api/auth/login`
   - Returns JWT token, phone number, and display name
   - Token stored in localStorage

3. **Protected Routes**: Dashboard requires valid token
   - If no token or token invalid, user redirected to login
   - Token automatically included in API headers via Axios interceptor

4. **Logout**: User logs out
   - Token and user info removed from localStorage
   - User redirected to login page

## 🔧 Configuration

### Backend URL

Edit `vite.config.js` to change the backend API URL:

```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',  // Change this URL
    changeOrigin: true,
  }
}
```

## 📱 Responsive Design

The application is fully responsive and works on:
- Desktop browsers
- Tablets
- Mobile devices

Breakpoints optimized for:
- Mobile: < 600px
- Tablet: 600px - 768px
- Desktop: > 768px

## 🎨 Styling

The app uses a modern green color scheme inspired by WhatsApp:
- Primary Color: #075e54 (Dark Green)
- Secondary Color: #25d366 (Green)
- Uses CSS variables for easy customization

## 🧪 Testing Authentication

### Sign Up (cURL)
```bash
curl -X POST http://localhost:3000/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+919876543210",
    "password": "Password123",
    "displayName": "John Doe"
  }'
```

### Login (cURL)
```bash
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+919876543210",
    "password": "Password123"
  }'
```

## ⚙️ API Endpoints

All requests use the proxy configured in `vite.config.js`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register new user |
| POST | `/api/auth/login` | Login user |
| GET | `/api/auth/chat` | Test authenticated endpoint |

## 📝 Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Create production build
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint (optional)

## 🚀 Deployment

1. Build the application:
```bash
npm run build
```

2. Deploy the `dist/` directory to your hosting service

3. Configure your backend URL in `vite.config.js` before building

## 📄 License

This project is open source and available for educational and commercial use.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

---

**Happy Coding! 🚀**
