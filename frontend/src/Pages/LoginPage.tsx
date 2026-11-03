import { useState } from 'react';
import LoginHeader from '../components/LoginHeader';
import LoginForm from '../components/LoginForm';
import RegisterForm from '../components/RegisterForm';
import bgImage from '../assets/zeroedBG.png';
import type { CurrentUser } from '../api';

interface LoginPageProps {
    onLogin: (user: CurrentUser) => void;
}

function LoginPage({ onLogin }: LoginPageProps) {
    const [isRegistering, setIsRegistering] = useState(false);

    return (
        <div className="d-flex flex-column vh-100 overflow-hidden" style={{
            backgroundImage: `url(${bgImage})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            backgroundRepeat: 'no-repeat'
        }}>

            <LoginHeader />

            <div className="container d-flex flex-column vh-100 flex-grow-1 justify-content-center">
                <div className="row justify-content-center">
                    <div className="col-md-6 col-lg-4">
                        <main className="form-container">
                            {isRegistering ? (
                                <RegisterForm onSwitchToLogin={() => setIsRegistering(false)} />
                            ) : (
                                <LoginForm onSwitchToRegister={() => setIsRegistering(true)} onLogin={onLogin} />
                            )}
                        </main>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default LoginPage
