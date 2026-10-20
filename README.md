# Zeroed

## Abstract

Zeroed is a web application with the purpose of simplifying shared expense tracking, implemented through bill splitting and debt settlement features. Our project is neither educational nor purely for entertainment; it is a practical utility tool whose purpose is to remove friction from a recurring financial chore. At the moment, casual expense tracking is widely handled through manual calculations, informal messaging, or at most spreadsheets. These methods are prone to calculation errors or even forgetfulness. Zeroed addresses this by offering an intuitive solution where users can form groups and log shared costs, while the app itself takes care of the majority of calculations. To streamline the process, the application will incorporate a mix of APIs. This includes a real-time currency exchange API to process multiple currencies, as well as a natural language classification API that automatically categorizes transactions (such as mapping entries like "Costco" or "Superstore" to groceries).

Tailored for college students, roommates, or anyone with an expense to track, Zeroed offers a convenient approach to managing finances.

## Proposed Systems Overview

The first system users will interact with will likely be accounts and authentication. Using an email and password, users are able to have personal accounts. The accounts are created by signing up, and used for authentication through logging in. They would likely be greeted by a set of data structures that will be used to organize the inserted expenses, as well as the way in which said information is presented, which would provide the system for their expenses. API integration will be very useful for their multiple possible applications, such as a currency exchange API for compatibility across multiple countries, and a natural language classification API for automatic transaction categorization for user sorting and convenience.

Users will also be able to create groups with others. This would allow multiple users to track shared expenses and manage settlements together, with the manual calculations being entirely set up by the application. Another way users would have interactions with each other is the balance and settlement system, which would keep track of the balance between different users, which user is in debt to which, as well as the amount that is owed. We will also have a system to send notifications to users in order to remind them of important information such as outstanding loans, via email, in-app, or push notification.

## Scope of Work

Zeroed centres on one main feature, tracking and splitting shared expenses. The project contains several subproblems: how a cost is divided among participants, how balances are settled, how groups are managed, and how entries are categorized. We broke the subproblems down into 5 epics which are: accounts and authentication, group management, expense entry & splitting engine, balance & settlement system and the reminders feature.

The most complex feature will be the expense entry and splitting engine. This feature is the most complex as it will handle various ways to split among your group such as via equal amounts, percentages, ratios, exact amounts, etc. It will also use a natural language classification API to categorize the type of transactions for all users of the transaction to see. It will handle rate limiting with a leaking bucket algorithm so users cannot spam create transactions/spam edit.

The second most complex system will be the balance and settlement system. This will show the debt of the user to whom they owe to, and in the correct type of currency using a currency conversion API. Users can settle their balance externally, and then send a confirmation to who they owed to confirm the transaction. After the transaction has been settled, past transactions will be stored.

The group system will handle inviting users to a group. You can create a group of people you would commonly use this app with without making a transaction and then add the group to transactions for quality of life rather than adding each user every time. You can also add or remove members of the group from the transaction without editing the group.

The account and authentication system will let users create an account with a username, email, password, and security question. It will have features for determining password strength, rate limiting through a sliding window algorithm in order to handle brute force password guessing, while still giving the user a good experience. Users may also login using Google's API.

The reminders/notifications feature will notify users through emails or web notifications. Users can notify those who owe them at any time but on a cooldown. Users can also choose options on reminder scheduling, such as once a day, once a week, custom days, etc. Whenever a transaction is edited, a reminder/notification will be sent to all users of the transaction, and settling of balances will notify the transaction starter.

## Member Roles

The five roles we will have for the project are ScrumMaster / PM, Backend Lead, Frontend Lead, Database / Integration / Deployment Lead, and Product Owner.

The ScrumMaster, Payden, will ensure that the team stays organized and on task. This includes organizing and leading the scrum meetings, managing the task board, and identifying blockers that are preventing progress.

The Frontend Lead, Landon, will design and improve the UI and UX of the web application. This includes using HTML, CSS, and Javascript to create the pages of the website, and designing an interface that is intuitive to use.

The Backend Lead, Matthew, will focus on server-side logic. This includes creating the Spring Boot endpoints, the authorization / login feature, and the data model.

The Database / Integration / Deployment Lead, Rafael will work on the database, API, and deployment. This includes creating the database schema, integrating the API with the web application, and overseeing the deployment of the site on Render.

The Product Owner, Felipe, represents the user's perspective. This includes maintaining the problem statement and user stories, prioritizing which features enter each iteration, and reviewing completed work against acceptance criteria before it is considered done.

## Competitive Analysis

Most groups do not begin with a dedicated application. They begin with a group chat and mental arithmetic. One person pays for dinner, someone types the total into the conversation, and everyone agrees to remember. This approach costs nothing and requires no sign-up, which is precisely why it persists, but the record it produces is not durable. The arithmetic lives inside the conversation, where it is interrupted by unrelated messages, scrolls out of reach, and cannot be searched reliably three weeks later when a trip ends. The arrangement also appoints someone, usually without asking, as the group's banker: they are expected to remember who paid for what and to raise the subject when money is owed. That turns a straightforward accounting problem into a social one, and it is the reason so many small debts are simply written off. Zeroed replaces the conversation-as-ledger with a persistent one. Every expense records who paid, who participated and how the cost was divided, all members see the same figures at the same time, and the balance is a computed result rather than a remembered one.

Tech-savvy users may turn to spreadsheets, but these run into their own unique set of issues. There is no consistency between one spreadsheet ledger and another, the user experience is mediocre. The biggest issue is the lack of reminders, which turns a simple accounting exercise back into a social one. Zeroed performs the accounting automatically, and treats settlement and reminders as features of the product rather than as favours from whoever owns the file.

Against dedicated applications, the two most widely used products are Splitwise and Tricount. Both are closed source. Splitwise limits unpaid accounts to four expense entries per day, carries advertising, and places core capabilities behind a subscription, while Tricount is free and ad-free but is operated by a bank, which makes it a funnel toward that institution's own financial products rather than a neutral record of a group's expenses. Our product delivers the capabilities Tricount highlights against Splitwise, including group expense tracking, multi-currency support, transaction categorization and balance settlement, without either compromise. Zeroed is open source, completely free, and free of both daily entry limits and advertising.
