# Generated by Django 3.2.5 on 2021-09-17 15:24

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('osrd_infra', '0005_tracksectionelectrificationtypecomponent'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='belongstolinecomponent',
            name='entity',
        ),
        migrations.RemoveField(
            model_name='belongstolinecomponent',
            name='line',
        ),
        migrations.RemoveField(
            model_name='belongstotrackcomponent',
            name='entity',
        ),
        migrations.RemoveField(
            model_name='belongstotrackcomponent',
            name='track',
        ),
        migrations.DeleteModel(
            name='LineEntity',
        ),
        migrations.DeleteModel(
            name='TrackEntity',
        ),
        migrations.AddField(
            model_name='tracksectioncomponent',
            name='line_code',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='tracksectioncomponent',
            name='line_name',
            field=models.CharField(default='', max_length=255),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='tracksectioncomponent',
            name='track_name',
            field=models.CharField(default='', max_length=255),
            preserve_default=False,
        ),
        migrations.AddField(
            model_name='tracksectioncomponent',
            name='track_number',
            field=models.IntegerField(default=0),
            preserve_default=False,
        ),
        migrations.DeleteModel(
            name='BelongsToLineComponent',
        ),
        migrations.DeleteModel(
            name='BelongsToTrackComponent',
        ),
    ]
