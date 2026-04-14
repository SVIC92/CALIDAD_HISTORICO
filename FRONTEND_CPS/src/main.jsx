import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { UISettingsProvider } from './context/UISettingsContext'
import { LoadingScreenProvider } from './context/LoadingScreenContext'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <UISettingsProvider>
      <BrowserRouter>
        <LoadingScreenProvider>
          <App />
        </LoadingScreenProvider>
      </BrowserRouter>
    </UISettingsProvider>
  </StrictMode>,
)
