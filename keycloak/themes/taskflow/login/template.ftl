<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Manrope:wght@400;500;600;700;800&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${url.resourcesPath}/css/login.css" />
</head>
<body>
    <div class="stage">
        <aside class="brand">
            <div class="brand-top">
                <div class="logo" aria-hidden="true">
                    <span class="mark"></span>
                    <span class="logo-type">
                        <strong>Taskflow</strong>
                        <em>Work platform</em>
                    </span>
                </div>
                <h1>Every project, in one place.</h1>
                <p>Plan work, assign tasks, and follow progress across your organization from a single secure sign-in.</p>
            </div>
            <div class="art" aria-hidden="true">
                <div class="card card-gold">
                    <span class="chip"></span>
                    <strong>Review</strong>
                    <small>Due Friday</small>
                </div>
                <div class="card card-ink">
                    <span class="chip light"></span>
                    <strong>Migration</strong>
                    <small>In progress</small>
                </div>
                <div class="sheet">
                    <div class="sheet-head">
                        <span>Project</span>
                        <b>Website refresh</b>
                    </div>
                    <div class="sheet-row"><span>Owner</span><b>Alice Acme</b></div>
                    <div class="sheet-row"><span>Status</span><b class="good">On track</b></div>
                    <div class="sheet-row"><span>Tasks</span><b>12 open</b></div>
                    <div class="bar"><i></i></div>
                </div>
            </div>
            <footer>© 2026 · Taskflow</footer>
        </aside>
        <main class="panel">
            <div class="form-wrap">
                <#nested "header">
                <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                    <div class="banner banner-${message.type}" role="status">
                        <span class="banner-icon" aria-hidden="true"></span>
                        <span>${kcSanitize(message.summary)?no_esc}</span>
                    </div>
                </#if>
                <#nested "form">
                <#if displayInfo>
                    <div class="info">
                        <#nested "info">
                    </div>
                </#if>
                <#nested "socialProviders">
            </div>
        </main>
    </div>
</body>
</html>
</#macro>
