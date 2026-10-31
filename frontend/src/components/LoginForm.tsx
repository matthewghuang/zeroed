import { useState } from 'react';

interface LoginFormProps {
    onSwitchToRegister: () => void;
}

function LoginForm({onSwitchToRegister}: LoginFormProps) {
        // arguments
        const [email, setEmail] = useState('');
        const [password, setPassword] = useState('');
    
        const handleForm = (e) => {
            e.preventDefault();
    
            fetch('/api/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            })
            .then(response => response.json(),
                error => console.error('Failed!', error)
            )
        }
    
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