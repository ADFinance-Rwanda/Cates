<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=false; section>
    <#if section = "header">
        <header class="intro">
            <h2>Sign in</h2>
            <p>Sign in with your organization account.</p>
        </header>
    <#elseif section = "form">
        <form id="kc-form-login" action="${url.loginAction}" method="post" onsubmit="this.login.disabled = true; return true;">
            <#if messagesPerField.existsError('username','password')>
                <div class="banner banner-error" role="alert">
                    <span class="banner-icon" aria-hidden="true"></span>
                    <span>${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</span>
                </div>
            </#if>

            <label for="username">Username <span class="req">*</span></label>
            <input id="username" name="username" type="text" autocomplete="username" autofocus
                   value="${(login.username!'')}"
                   aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
                   placeholder="Enter your username" />

            <label for="password">Password <span class="req">*</span></label>
            <div class="password-field">
                <input id="password" name="password" type="password" autocomplete="current-password"
                       aria-invalid="<#if messagesPerField.existsError('username','password')>true<#else>false</#if>"
                       placeholder="Enter your password" />
                <button type="button" class="reveal" aria-label="Show password" onclick="togglePassword(this)">
                    <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/></svg>
                </button>
            </div>

            <input type="hidden" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
            <button class="submit" name="login" type="submit">${msg("doLogIn")}</button>
        </form>
        <script>
            function togglePassword(button) {
                var input = document.getElementById('password');
                var show = input.type === 'password';
                input.type = show ? 'text' : 'password';
                button.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
                button.classList.toggle('is-on', show);
            }
        </script>
    </#if>
</@layout.registrationLayout>
