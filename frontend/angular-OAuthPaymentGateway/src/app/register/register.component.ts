import { Component, OnInit } from '@angular/core';
import { AuthService } from '../_services/auth.service';
import { DomSanitizer } from '@angular/platform-browser';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

  form: any = {};
  isSuccessful = false;
  isSignUpFailed = false;
  isUsing2FA = false;
  errorMessage = '';
  qrCodeImage = '';
  realCaptcha = '';
  hiddenCaptcha = '';

  constructor(private authService: AuthService, public _DomSanitizationService: DomSanitizer) { }

  ngOnInit(): void {
    this.authService.getCaptcha().subscribe(
      data => {
        console.log(data)
        this.realCaptcha = "data:image/jpg;base64, " + data.realCaptcha;
        this.hiddenCaptcha = data.hiddenCaptcha
      },
      err => {
        this.errorMessage = err.error.message;
      }
    )
  }

  onSubmit(): void {
    this.form.hiddenCaptcha = this.hiddenCaptcha
    // console.log(this.form);
    this.authService.register(this.form).subscribe(
      data => {
        console.log(data);
        if(data.using2FA){
        	this.isUsing2FA = true;
        	this.qrCodeImage = data.qrCodeImage;
        }
	    this.isSuccessful = true;
        this.isSignUpFailed = false;
      },
      err => {
        this.errorMessage = err.error.message;
        this.isSignUpFailed = true;
      }
    );
  }

  callFunction(event){
    this.authService.getCaptcha().subscribe(
      data => {
        this.realCaptcha = "data:image/jpg;base64, " + data.realCaptcha;
        this.hiddenCaptcha = data.hiddenCaptcha
      },
      err => {
        this.errorMessage = err.error.message;
        this.isSignUpFailed = true;
      }
    )
  }

}
