import axios from "axios"

export interface CurrentUser {
  email: string;
  role: 'REGULAR' | 'ADMIN';
}

export default axios.create({
  withCredentials:true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN"
});
