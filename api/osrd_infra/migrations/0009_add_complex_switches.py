# Generated by Django 3.1.6 on 2021-09-27 15:04

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0008_geolineslocationcomponent'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='switchcomponent',
            name='left',
        ),
        migrations.RemoveField(
            model_name='switchcomponent',
            name='right',
        ),
        migrations.AddField(
            model_name='switchcomponent',
            name='links',
            field=models.JSONField(default=list),
        ),
        migrations.AlterField(
            model_name='switchpositioncomponent',
            name='position',
            field=models.IntegerField(),
        ),
    ]