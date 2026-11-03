import LoginHeader from './LoginHeader';

function LoginPage() {

  return (
    <div>
        <LoginHeader />
        <div className="body-content center">
            <div className="login-container">
                <form className="login-form" action="">
                    <div className="form-group">
                        <label htmlFor="username">Username:</label>
                        <input type="text" id="username"></input>
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Password:</label>
                        <input type="text" id="password"></input>
                    </div>
                    <button type="submit" className="btn btn-primary">
                        Log In
                    </button>
                </form>
            </div>
        </div>
    </div>
  )
}

export default LoginPage
