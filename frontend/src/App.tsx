import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './Pages/LoginPage';
import GroupPage from './Pages/GroupPage';
import ExpensePage from './Pages/ExpensePage';

function App() {

  return (
    <BrowserRouter>
        <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/groups" element={<GroupPage />} />
            <Route path="/expenses" element={<ExpensePage />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
    </BrowserRouter>
    )
}

export default App
