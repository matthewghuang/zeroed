interface LoginNotifProps {
    message: string;
    type?: 'error' | 'success'; // New prop to switch styling/headers
}

function LoginNotif({ message, type = 'error' }: LoginNotifProps) {
    const isSuccess = type === 'success';

    return (
    <div className="login-container card border border-secondary text-white mb-3 shadow-lg p-4" style={{backgroundColor: '#463647'}}>
        <h3 className="text-center">{isSuccess ? 'Success' : 'Error'}</h3>
        <p className={`text-center ${isSuccess ? 'text-success' : 'text-danger'} mt-2 mb-0`}>
            {message || (isSuccess ? "Success!" : "An error occurred during authentication.")}
        </p>
    </div>
    )
}

export default LoginNotif