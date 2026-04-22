import axios, { AxiosRequestConfig, AxiosResponse } from "axios";
import type { LoginResponse } from "./modules/auth";
import { clearAuthState, getAuthState, setAuthState } from "../store/auth-store";
import type { ApiResponse } from "../types/api";

const instance = axios.create({
  baseURL: "/api",
  timeout: 10000
});

const refreshInstance = axios.create({
  baseURL: "/api",
  timeout: 10000
});

let refreshPromise: Promise<string | null> | null = null;

instance.interceptors.request.use((config) => {
  const token = getAuthState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

function isRefreshRequest(config?: AxiosRequestConfig) {
  return config?.url === "/auth/refresh";
}

function normalizeError(error: unknown) {
  const message = axios.isAxiosError(error)
    ? error.response?.data?.message ?? error.message ?? "请求失败"
    : error instanceof Error
      ? error.message
      : "请求失败";
  return new Error(message);
}

async function refreshAccessToken() {
  const auth = getAuthState();
  if (!auth.refreshToken) {
    clearAuthState();
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = refreshInstance
      .post<ApiResponse<LoginResponse>>("/auth/refresh", {
        refresh_token: auth.refreshToken
      })
      .then((response) => {
        const payload = response.data;
        if (payload.code !== 0 || !payload.data) {
          throw new Error(payload.message);
        }

        setAuthState({
          token: payload.data.token,
          refreshToken: payload.data.refresh_token,
          user: payload.data.user
        });
        return payload.data.token;
      })
      .catch((error) => {
        clearAuthState();
        throw normalizeError(error);
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

async function unwrapResponse<T>(response: AxiosResponse<ApiResponse<T>>, config?: AxiosRequestConfig) {
  const payload = response.data;
  if (payload.code === 401 && !isRefreshRequest(config)) {
    const nextToken = await refreshAccessToken().catch(() => null);
    if (nextToken) {
      const retriedResponse = await instance.request<ApiResponse<T>>({
        ...config,
        headers: {
          ...config?.headers,
          Authorization: `Bearer ${nextToken}`
        }
      });
      return unwrapResponse(retriedResponse, { ...config, url: "/auth/refresh" });
    }
  }

  if (payload.code !== 0) {
    if (payload.code === 401) {
      clearAuthState();
    }
    throw new Error(payload.message);
  }
  return payload.data;
}

async function request<T>(config: AxiosRequestConfig) {
  try {
    const response = await instance.request<ApiResponse<T>>(config);
    return await unwrapResponse(response, config);
  } catch (error) {
    throw normalizeError(error);
  }
}

const client = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({ ...config, method: "get", url });
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, method: "post", url, data });
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return request<T>({ ...config, method: "put", url, data });
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return request<T>({ ...config, method: "delete", url });
  }
};

export default client;
