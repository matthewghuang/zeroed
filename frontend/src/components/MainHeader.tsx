import logoImage from '../assets/zeroedIconLeft.png';

function LoginHeader() {
    return (
        <header className="login-header d-flex border-bottom border-black shadow-lg" style={{ 
                height: '60px', 
                backgroundColor: '#281e2b'
            }}>
            <img 
                src={logoImage} 
                alt="Zeroed Logo" 
                style={{ height: '100%', width: 'auto', objectFit: 'contain' }} 
            />
        </header>
    );
}

export default LoginHeader