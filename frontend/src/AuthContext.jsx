import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('artsight_token'));
  const [email, setEmail] = useState(() => localStorage.getItem('artsight_email'));

  const login = (newToken, userEmail) => {
    localStorage.setItem('artsight_token', newToken);
    localStorage.setItem('artsight_email', userEmail);
    setToken(newToken);
    setEmail(userEmail);
  };

  const logout = () => {
    localStorage.removeItem('artsight_token');
    localStorage.removeItem('artsight_email');
    setToken(null);
    setEmail(null);
  };

  return (
    <AuthContext.Provider value={{ token, email, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
