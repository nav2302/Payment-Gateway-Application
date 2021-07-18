import { HTTP_INTERCEPTORS, HttpEvent } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpHandler, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { TokenStorageService } from '../_services/token-storage.service';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { AuthService } from '../_services/auth.service';

const TOKEN_HEADER_KEY = 'Authorization';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
	constructor(private token: TokenStorageService, private router: Router, private auth: AuthService) {

	}

	intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
		let authReq = req;
		const loginPath = '/login';
		// const token = this.token.getToken();
			// authReq = req.clone({ headers: req.headers.set(TOKEN_HEADER_KEY, 'Bearer ' + token) });
		authReq = req.clone({ withCredentials: true });
		return next.handle(authReq).pipe(tap(() => { },
			(err: any) => {
				if (err instanceof HttpErrorResponse) {
					if (err.status !== 401 || window.location.pathname === loginPath) {
						return;
					}
					this.token.signOut().subscribe(
						data => { 
							console.log(data.headers);
							window.location.href = loginPath;
						}, 
						err => {
							console.log(err);
						}
					);
				}
			}
		));
	}
}

export const authInterceptorProviders = [
	{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
];
