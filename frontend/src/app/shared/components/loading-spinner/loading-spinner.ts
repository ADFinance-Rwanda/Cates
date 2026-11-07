import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  template: `<p class="loading" role="status">{{ message() }}</p>`
})
export class LoadingSpinner {
  readonly message = input('Loading…');
}
