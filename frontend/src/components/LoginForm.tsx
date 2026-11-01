import { useState } from 'react';
import api from "../api";

interface LoginFormProps {
    onSwitchToRegister: () => void;
}

function LoginForm({onSwitchToRegister}: LoginFormProps) {
    // arguments
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleForm = async (e) => {
        e.preventDefault();

        try {
            await api.get('/api/auth/csrf');

            const response = await api.post('/api/auth/login', { email, password });
            console.log('Authentication successful', response.data);
        } catch (error) {
            console.error('Authentication failed', error);
        }
    };
    
    return (
    <div className="login-container card mb-3 shadow p-4">
        <h2 className="text-center">Login</h2>
        <form className="login-form" onSubmit={handleForm}>
            <div className="form-group">
                <label htmlFor="email" className="form-label">Email:</label>
                <input 
                    type="text" 
                    id="email" 
                    className="form-control"
                    value={email} 
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
                    onChange={(e) => setPassword(e.target.value)}
                ></input>
            </div>
            <div className="text-center mt-3 mb-3">
                <button type="submit" className="btn btn-primary text-center">
                    Log In
                </button>
            </div>
            <p className="text-center">
                Don't have an account? <button type="button" onClick={onSwitchToRegister} className="btn-link">Sign up</button>
            </p>
        </form>
    </div>
    )
}

export default LoginForm