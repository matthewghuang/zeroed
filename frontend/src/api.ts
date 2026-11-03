import axios from "axios"

export default axios.create({
  withCredentials:true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN"
});