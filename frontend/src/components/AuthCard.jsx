export function AuthCard({ title, subtitle, children, footer }) {
  return (
    <section className="auth-card">
      <div className="auth-card-header">
        <h1>{title}</h1>
        {subtitle ? <p>{subtitle}</p> : null}
      </div>
      {children}
      {footer ? <div className="auth-footer">{footer}</div> : null}
    </section>
  );
}