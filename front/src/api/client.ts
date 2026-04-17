import axios, { AxiosRequestConfig, AxiosResponse } from "axios";
import { clearAuthState, getAuthState } from "../store/auth-store";
import type { ApiResponse } from "../types/api";

const instance = axios.create({
  baseURL: "/api",
  timeout: 10000
});

instance.interceptors.request.use((config) => {
  const token = getAuthState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>) {
  const payload = response.data;
  if (payload.code !== 0) {
    if (payload.code === 401) {
      clearAuthState();
    }
    throw new Error(payload.message);
  }
  return payload.data;
}

function handleError(error: unknown): Promise<never> {
  const message = axios.isAxiosError(error)
    ? error.response?.data?.message ?? error.message ?? "请求失败"
    : "请求失败";
  return Promise.reject(new Error(message));
}

const client = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return instance.get<ApiResponse<T>>(url, config).then(unwrapResponse).catch(handleError);
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.post<ApiResponse<T>>(url, data, config).then(unwrapResponse).catch(handleError);
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return instance.put<ApiResponse<T>>(url, data, config).then(unwrapResponse).catch(handleError);
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return instance.delete<ApiResponse<T>>(url, config).then(unwrapResponse).catch(handleError);
  }
};

export default client;
