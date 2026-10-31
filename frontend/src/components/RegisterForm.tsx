interface RegisterFormProps {
    onSwitchToLogin: () => void;
}

function RegisterForm({onSwitchToLogin}: RegisterFormProps) {
    return (
    <div className="login-container">
        <form className="login-form" action="">
            <div className="form-group">
                <label htmlFor="email">Email:</label>
                <input type="text" id="email" name="email"></input>
            </div>
            <div className="form-group">
                <label htmlFor="password">Password:</label>
                <input type="password" id="password" name="password"></input>
            </div>
            <button type="submit" className="btn btn-primary">
                Log In
            </button>
            <p>
                Already have an account? <button type="button" onClick={onSwitchToLogin} className="btn-link">Login</button>
            </p>
        </form>
    </div>
    )
}

export default RegisterForm