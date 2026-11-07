import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirmation-dialog',
  template: `
    @if (open()) {
      <div class="backdrop" (click)="cancel.emit()">
        <div class="modal" role="alertdialog" aria-modal="true" [attr.aria-label]="title()" (click)="$event.stopPropagation()">
          <h2>{{ title() }}</h2>
          <p>{{ message() }}</p>
          <div class="modal-actions">
            <button type="button" class="secondary" (click)="cancel.emit()">Cancel</button>
            <button type="button" class="danger" (click)="confirm.emit()">{{ confirmLabel() }}</button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmationDialog {
  readonly open = input(false);
  readonly title = input('Confirm');
  readonly message = input('');
  readonly confirmLabel = input('Confirm');
  readonly confirm = output<void>();
  readonly cancel = output<void>();
}
