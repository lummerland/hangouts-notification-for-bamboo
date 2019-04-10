# Hangouts Notifications for Bamboo

Let Bamboo send your build results to your Google Hangouts Chat.

Just add a webhook to the Google Hangouts room where you want to see built result messages as described here:
https://developers.google.com/hangouts/chat/how-tos/webhooks#define_an_incoming_webhook

Copy the webhook URL, add a new notification to your build plan in Bamboo, choose "Hangouts" as notification type and add the webhook URL there. More about notifications in Bamboo and how to add them to you build plan you can find here: https://confluence.atlassian.com/bamboo/configuring-notifications-for-a-plan-and-its-jobs-289276973.html

![Bamboo Notification in Hangouts Chat](src/main/resources/images/screenshot-1-0-0.png)

## Things to come / ideas

* Configure which elements should be displayed in the notification
