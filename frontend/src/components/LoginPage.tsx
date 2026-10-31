import { useState } from 'react'
import LoginHeader from './LoginHeader';

function LoginPage() {

  return (
    <div>
        <LoginHeader />
        <div className="body-content">
            <div className="login-container">
                <form className="login-form" action="">
                    <div className="form-group">
                        <label htmlFor="username">Username:</label>
                        <input type="text" id="username"></input>
                    </div>
                </form>
            </div>
        </div>
    </div>
  )
}

export default LoginPage
