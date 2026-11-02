import logoImage from '../assets/zeroedIconLeft.png';

function LoginHeader() {
    return (
        <header className="login-header d-flex border-bottom border-black shadow-lg" style={{ 
                height: '60px', 
                backgroundColor: '#281e2b'
            }}>
            <div className="d-flex align-items-center">
                <img 
                    src={logoImage} 
                    alt="Zeroed Logo" 
                    style={{ height: '100%', width: 'auto', objectFit: 'contain' }} 
                />
            </div>
            <div className="mb-2 border-bottom ms-auto">
                <div className="d-flex gap-2">
                    <button className="btn btn-outline-light btn-sm">Expenses</button>
                    <button className="btn btn-outline-light btn-sm">Groups</button>
                </div>
            </div>
            <div className="d-flex gap-2 ms-auto">
                <button className="btn btn-outline-light btn-sm">Balance</button>
                <button className="btn btn-outline-light btn-sm">Notifs</button>
                <button className="btn btn-outline-light btn-sm">Profile</button>
            </div>
        </header>
    );
}

export default LoginHeader