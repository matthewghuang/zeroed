import axios from "axios"

export default axios.create({
  baseURL: "http://localhost:8080",
  withCredentials:true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN"
});