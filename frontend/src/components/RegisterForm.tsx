import { useState } from 'react';
import api from "../api";

interface RegisterFormProps {
    onSwitchToLogin: () => void;
}

function RegisterForm({onSwitchToLogin}: RegisterFormProps) {
    // arguments
    const [email, setEmail] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    const handleForm = async (e) => {
        e.preventDefault();

        try {
            await api.get('/api/auth/csrf');

            const response = await api.post('/api/auth/register', { username, email, password });
            console.log('Authentication successful', response.data);
        } catch (error) {
            console.error('Authentication failed', error);
        }
    };

    return (
    <div className="login-container">
        <h2>Register</h2>
        <form className="login-form" onSubmit={handleForm}>

            <div className="form-group">
                <label htmlFor="username">Username:</label>
                <input 
                    type="text" 
                    id="username" 
                    value={username} 
                    onChange={(e) => setUsername(e.target.value)}
                ></input>
            </div>

            <div className="form-group">
                <label htmlFor="email">Email:</label>
                <input 
                    type="email" 
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
                Sign Up
            </button>
            <p>
                Already have an account? <button type="button" onClick={onSwitchToLogin} className="btn-link">Login</button>
            </p>
        </form>
    </div>
    )
}

export default RegisterForm