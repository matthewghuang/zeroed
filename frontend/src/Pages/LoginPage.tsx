import { useState } from 'react';
import LoginHeader from '../components/LoginHeader';
import LoginForm from '../components/LoginForm';
import RegisterForm from '../components/RegisterForm';

function LoginPage() {
    const [isRegistering, setIsRegistering] = useState(false);
    
    return (
    <div>
        <LoginHeader />
        <div className="container mt-5">
            <div className="row justify-content-center">
                <div className="col-md-6 col-lg-4">
                    <main className="form-container">
                        {isRegistering ? (
                        <RegisterForm onSwitchToLogin={() => setIsRegistering(false)} />
                        ) : (
                        <LoginForm onSwitchToRegister={() => setIsRegistering(true)} />
                        )}
                    </main>
                </div>
            </div>
        </div>
    </div>
  )
}

export default LoginPage
