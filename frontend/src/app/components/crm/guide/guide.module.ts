import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { GuideComponent } from './guide.component';
import { FlexLayoutModule } from '@angular/flex-layout';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
@NgModule({
  declarations: [
    GuideComponent
  ],
  exports: [
    GuideComponent
  ],
  imports: [
    CommonModule,
    FlexLayoutModule,
    MatStepperModule,
    MatButtonModule,
    MatIconModule
  ]
})
export class GuideModule { }
