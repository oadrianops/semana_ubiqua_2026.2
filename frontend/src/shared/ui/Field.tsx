import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';
import { cn } from '../lib/cn';

const control =
  'block w-full min-h-12 rounded-[var(--radius-control)] border border-line-strong bg-surface px-3 text-base text-ink ' +
  'placeholder:text-muted aria-[invalid=true]:border-danger';

interface FieldShellProps {
  label: ReactNode;
  hint?: ReactNode;
  error?: string | null;
  children: (ids: { id: string; describedBy: string | undefined; invalid: boolean }) => ReactNode;
  className?: string;
}

/** Campo com rótulo, dica e erro associados (14.5): o erro diz o que fazer. */
export function FieldShell({ label, hint, error, children, className }: FieldShellProps) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;
  return (
    <div className={cn('grid gap-1.5', className)}>
      <label htmlFor={id} className="text-base font-semibold text-ink">
        {label}
      </label>
      {hint && (
        <p id={hintId} className="text-sm text-support">
          {hint}
        </p>
      )}
      {children({ id, describedBy, invalid: Boolean(error) })}
      {error && (
        <p id={errorId} className="text-sm font-semibold text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label: ReactNode;
  hint?: ReactNode;
  error?: string | null;
};

export const TextField = forwardRef<HTMLInputElement, InputProps>(function TextField(
  { label, hint, error, className, ...rest },
  ref,
) {
  return (
    <FieldShell label={label} hint={hint} error={error} className={className}>
      {({ id, describedBy, invalid }) => (
        <input
          ref={ref}
          id={id}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className={control}
          {...rest}
        />
      )}
    </FieldShell>
  );
});

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: ReactNode;
  hint?: ReactNode;
  error?: string | null;
};

export const SelectField = forwardRef<HTMLSelectElement, SelectProps>(function SelectField(
  { label, hint, error, className, children, ...rest },
  ref,
) {
  return (
    <FieldShell label={label} hint={hint} error={error} className={className}>
      {({ id, describedBy, invalid }) => (
        <select
          ref={ref}
          id={id}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className={cn(control, 'pr-8')}
          {...rest}
        >
          {children}
        </select>
      )}
    </FieldShell>
  );
});

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: ReactNode;
  hint?: ReactNode;
  error?: string | null;
};

export const TextareaField = forwardRef<HTMLTextAreaElement, TextareaProps>(function TextareaField(
  { label, hint, error, className, ...rest },
  ref,
) {
  return (
    <FieldShell label={label} hint={hint} error={error} className={className}>
      {({ id, describedBy, invalid }) => (
        <textarea
          ref={ref}
          id={id}
          aria-describedby={describedBy}
          aria-invalid={invalid || undefined}
          className={cn(control, 'min-h-28 py-2')}
          {...rest}
        />
      )}
    </FieldShell>
  );
});

type CheckboxProps = InputHTMLAttributes<HTMLInputElement> & { label: ReactNode; error?: string | null };

export const CheckboxField = forwardRef<HTMLInputElement, CheckboxProps>(function CheckboxField(
  { label, error, className, ...rest },
  ref,
) {
  const id = useId();
  return (
    <div className={cn('grid gap-1', className)}>
      <label htmlFor={id} className="flex min-h-11 cursor-pointer items-start gap-3 py-1 text-base text-ink">
        <input
          ref={ref}
          id={id}
          type="checkbox"
          aria-invalid={Boolean(error) || undefined}
          aria-describedby={error ? `${id}-error` : undefined}
          className="mt-1 size-5 shrink-0 accent-[var(--acai-700)]"
          {...rest}
        />
        <span>{label}</span>
      </label>
      {error && (
        <p id={`${id}-error`} className="text-sm font-semibold text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
});
