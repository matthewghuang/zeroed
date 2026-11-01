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
    <div className="login-container card border border-secondary text-white mb-3 shadow-lg p-4" style={{backgroundColor: '#342835'}}>
        <h2 className="text-center">Register</h2>
        <form className="login-form" onSubmit={handleForm}>
            <div className="form-group">
                <label htmlFor="username" className="form-label">Username:</label>
                <input 
                    type="text" 
                    id="username" 
                    className="form-control"
                    value={username} 
                    placeholder='username'
                    onChange={(e) => setUsername(e.target.value)}
                ></input>
            </div>

            <div className="form-group">
                <label htmlFor="email" className="form-label">Email:</label>
                <input 
                    type="email" 
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
                Sign Up
            </button>
            </div>
            <p className="text-center">
                Already have an account? <button type="button" onClick={onSwitchToLogin} className="btn-link">Login</button>
            </p>
        </form>
    </div>
    )
}

export default RegisterForm