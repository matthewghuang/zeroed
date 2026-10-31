import { useState } from 'react';
import LoginHeader from '../components/LoginHeader';
import LoginForm from '../components/LoginForm';
import RegisterForm from '../components/RegisterForm';

function LoginPage() {
    const [isRegistering, setIsRegistering] = useState(false);
    
    return (
    <div>
        <LoginHeader />
        <div className="body-content center">
            <main className="form-container">
                {isRegistering ? (
                <RegisterForm onSwitchToLogin={() => setIsRegistering(false)} />
                ) : (
                <LoginForm onSwitchToRegister={() => setIsRegistering(true)} />
                )}
            </main>
        </div>
    </div>
  )
}

export default LoginPage
