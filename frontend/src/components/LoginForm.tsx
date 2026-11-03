import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import api from "../api";
import LoginNotif from './ErrorNotif';

interface LoginFormProps {
    onSwitchToRegister: () => void;
}

function LoginForm({ onSwitchToRegister }: LoginFormProps) {
    // arguments
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errorMessage, setErrorMessage] = useState('');

    const navigate = useNavigate();

    const handleForm = async (e: any) => {
        e.preventDefault();

        try {
            setErrorMessage('');
            await api.get('/api/auth/csrf');

            const response = await api.post('/api/auth/login', { email, password });
            console.log('Authentication successful', response.data);
            navigate("/groups")
        } catch (error: any) {
            console.error('Authentication failed', error);
            const msg = error.response?.data?.message || "Login failed. Check your email and password."
            setErrorMessage(msg);
        }
    };

    return (
        <div className="login-container card border border-secondary text-white mb-3 shadow-lg p-4" style={{ backgroundColor: '#342835' }}>
            <h2 className="text-center">Login</h2>
            <form className="login-form" onSubmit={handleForm}>
                <div className="form-group">
                    <label htmlFor="email" className="form-label">Email:</label>
                    <input
                        type="text"
                        id="email"
                        className="form-control"
                        value={email}
                        placeholder='email'
                        onChange={(e) => setEmail(e.target.value)}
                    ></input>
                </div>

                <div className="form-group">
                    <label htmlFor="password" className="form-label">Password:</label>
                    <input
                        type="password"
                        id="password"
                        className="form-control"
                        value={password}
                        placeholder='password'
                        onChange={(e) => setPassword(e.target.value)}
                    ></input>
                </div>
                <div className="text-center mt-3 mb-3">
                    <button type="submit" className="btn btn-secondary text-center">
                        Log In
                    </button>
                </div>
                <p className="text-center">
                    Don't have an account? <button type="button" onClick={onSwitchToRegister} className="btn-link">Sign up</button>
                </p>
                {errorMessage && <LoginNotif message={errorMessage} />}
            </form>
        </div>
    )
}

export default LoginForm