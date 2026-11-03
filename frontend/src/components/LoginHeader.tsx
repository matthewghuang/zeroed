import logoImage from '../assets/zeroedIconCenter.png';

function LoginHeader() {
    return (
        <header className="login-header d-flex justify-content-center align-items-center border-bottom border-black shadow-lg" style={{ 
                height: '60px', 
                backgroundColor: '#342835'
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
