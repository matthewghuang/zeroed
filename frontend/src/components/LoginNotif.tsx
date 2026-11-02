function LoginNotif({ message }) {
    return (
    <div className="login-container card border border-secondary text-white mb-3 shadow-lg p-4" style={{backgroundColor: '#463647'}}>
        <h3 className="text-center">Error</h3>
        <p className="text-center text-danger mt-2 mb-0">
            {message || "An error occurred during authentication."}
        </p>
    </div>
    )
}

export default LoginNotif