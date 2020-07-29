import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'humanizeEnum'
})
export class HumanizeEnumPipe implements PipeTransform {

  transform(value: string): any {
    value = value.split('_').join(' ');
    value = value.toLowerCase();
    return value.charAt(0).toUpperCase() + value.slice(1);
  }

}
