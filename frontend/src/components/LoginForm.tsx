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
    <div className="login-container">
        <h2>Login</h2>
        <form className="login-form" onSubmit={handleForm}>

            <div className="form-group">
                <label htmlFor="email">Email:</label>
                <input 
                    type="text" 
                    id="email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)}
                ></input>
            </div>

            <div className="form-group">
                <label htmlFor="password">Password:</label>
                <input 
                    type="password" 
                    id="password" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)}
                ></input>
            </div>

            <button type="submit" className="btn btn-primary">
                Log In
            </button>
            <p>
                Don't have an account? <button type="button" onClick={onSwitchToRegister} className="btn-link">Sign up</button>
            </p>
        </form>
    </div>
    )
}

export default LoginForm