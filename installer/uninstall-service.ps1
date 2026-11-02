$ErrorActionPreference = 'SilentlyContinue'
if (Get-ScheduledTask -TaskName 'CarWashApi') {
    Stop-ScheduledTask -TaskName 'CarWashApi'
    Unregister-ScheduledTask -TaskName 'CarWashApi' -Confirm:$false
}
Get-NetFirewallRule -DisplayName 'CarWash API LAN*' | Remove-NetFirewallRule
