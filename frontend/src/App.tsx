import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './Pages/LoginPage';
import GroupPage from './Pages/GroupPage';
import { useState } from 'react';
import type { CurrentUser } from './api';

function App() {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);

  return (
    <BrowserRouter>
        <Routes>
            <Route path="/login" element={<LoginPage onLogin={setCurrentUser} />} />
            <Route path="/groups" element={<GroupPage currentUser={currentUser} onUserLoaded={setCurrentUser} />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    </BrowserRouter>
    )
}

export default App
