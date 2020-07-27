import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LeadsComponent } from './leads.component';

import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { ClipboardModule } from '@angular/cdk/clipboard';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { MatListModule } from '@angular/material/list';
import { MatSelectModule } from '@angular/material/select';


import { MatTableExporterModule } from 'mat-table-exporter';


@NgModule({
  declarations: [ LeadsComponent ],
  exports: [ LeadsComponent,
    MatButtonModule,
       MatIconModule,
       MatTableModule,

       MatInputModule,
       MatFormFieldModule,
       MatInputModule,
       MatCheckboxModule,
       ClipboardModule,
       MatDialogModule,
       MatProgressSpinnerModule,
       MatCardModule,
       MatCheckboxModule,
       MatDialogModule,
       MatListModule,
       MatSelectModule,
       MatPaginatorModule,
   ],
  imports: [
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatInputModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    ClipboardModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatCheckboxModule,
    MatDialogModule,
    MatListModule,
    MatSelectModule,
    MatTableExporterModule,
    CommonModule
  ]
})
export class LeadsModule { }
