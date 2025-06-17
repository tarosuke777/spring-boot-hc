# CloudFormation

## create

aws cloudformation deploy \
 --template-file message-table.yaml \
 --stack-name MessageTableStack \
 --region ap-northeast-1

## delete

aws cloudformation delete-stack \
 --stack-name MessageTableStack \
 --region ap-northeast-1

# DynamoDB

## create

aws dynamodb create-table \
 --table-name message \
 --attribute-definitions \
 AttributeName=createdAt,AttributeType=S \
 AttributeName=channelId,AttributeType=S \
 --key-schema \
 AttributeName=channelId,KeyType=HASH \
 AttributeName=createdAt,KeyType=RANGE \
 --provisioned-throughput \
 ReadCapacityUnits=1,WriteCapacityUnits=1 \
 --region ap-northeast-1

## delete

aws dynamodb delete-table \
 --table-name message \
 --region ap-northeast-1

## list

aws dynamodb list-tables --region ap-northeast-1

## DynamoDBLocal

## create

aws dynamodb create-table \
 --table-name message \
 --attribute-definitions \
 AttributeName=createdAt,AttributeType=S \
 AttributeName=channelId,AttributeType=S \
 --key-schema \
 AttributeName=channelId,KeyType=HASH \
 AttributeName=createdAt,KeyType=RANGE \
 --provisioned-throughput \
 ReadCapacityUnits=1,WriteCapacityUnits=1 \
 --region ddblocal \
 --endpoint-url http://localhost:18000

## delete

aws dynamodb delete-table \
 --table-name message \
 --region ddblocal \
 --endpoint-url http://localhost:18000

## list

aws dynamodb list-tables \
 --endpoint-url http://localhost:18000 \
 --region ddblocal
